#!/usr/bin/env python3

"""
Extract Minecraft recipe JSON files from:

    * a Minecraft .jar
    * mod .jar files
    * datapack .zip files
    * an extracted datapack directory
    * a world save's datapacks directory

The result is a JSON database containing:

    - recipe id
    - recipe type
    - output
    - output count
    - every ingredient alternative

This is an OFFLINE extractor.

The in-game Fabric mod should still use Minecraft's RecipeManager
as the authoritative source because that is what the actual world/server
has loaded.
"""

from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path
from typing import Any, Dict, Iterable, List, Set, Tuple


Recipe = Dict[str, Any]


def plain_id(value: str) -> str:
    """
    Convert minecraft:item to item.

    Keep mod namespaces:

        create:andesite_alloy
    """
    if value.startswith("minecraft:"):
        return value[len("minecraft:"):]

    return value


def normalize_id(value: str) -> str:
    if ":" not in value:
        return "minecraft:" + value

    return value


def read_json_from_zip(
    archive: zipfile.ZipFile,
    name: str,
) -> Any:
    with archive.open(name) as stream:
        return json.load(stream)


def collect_archives(
    minecraft: Path | None,
    mods_dir: Path | None,
    world: Path | None,
    datapacks: Path | None,
) -> List[Tuple[str, Path]]:
    result: List[Tuple[str, Path]] = []

    if minecraft and minecraft.is_file():
        result.append(("minecraft", minecraft))

    if mods_dir and mods_dir.is_dir():
        for path in sorted(mods_dir.glob("*.jar")):
            result.append((f"mod:{path.name}", path))

    if datapacks and datapacks.exists():
        for path in sorted(datapacks.iterdir()):
            if path.suffix.lower() == ".zip":
                result.append((f"datapack:{path.name}", path))

    if world:
        world_datapacks = world / "datapacks"

        if world_datapacks.is_dir():
            for path in sorted(world_datapacks.iterdir()):
                if path.suffix.lower() == ".zip":
                    result.append(
                        (
                            f"world-datapack:{path.name}",
                            path,
                        )
                    )

    return result


def recipe_files_in_archive(
    archive: zipfile.ZipFile,
) -> Iterable[str]:
    for name in archive.namelist():
        if (
            name.startswith("data/")
            and "/recipes/" in name
            and name.endswith(".json")
        ):
            yield name


def tag_files_in_archive(
    archive: zipfile.ZipFile,
) -> Iterable[str]:
    for name in archive.namelist():
        if (
            name.startswith("data/")
            and "/tags/items/" in name
            and name.endswith(".json")
        ):
            yield name


def namespace_and_path(
    archive_path: str,
) -> Tuple[str, str] | None:
    parts = archive_path.split("/")

    if len(parts) < 4:
        return None

    if parts[0] != "data":
        return None

    namespace = parts[1]

    return namespace, "/".join(parts[2:])


class ResourceDatabase:
    def __init__(self) -> None:
        self.tags: Dict[str, Dict[str, Any]] = {}

    def add_tag(
        self,
        tag_id: str,
        data: Dict[str, Any],
    ) -> None:
        self.tags[normalize_id(tag_id)] = data

    def resolve_tag(
        self,
        tag_id: str,
        seen: Set[str] | None = None,
    ) -> List[str]:
        tag_id = normalize_id(tag_id)

        if seen is None:
            seen = set()

        if tag_id in seen:
            return []

        seen.add(tag_id)

        data = self.tags.get(tag_id)

        if data is None:
            return []

        result: List[str] = []

        for entry in data.get("values", []):
            if isinstance(entry, str):
                if entry.startswith("#"):
                    result.extend(
                        self.resolve_tag(
                            entry[1:],
                            seen.copy(),
                        )
                    )
                else:
                    result.append(
                        plain_id(
                            normalize_id(entry)
                        )
                    )

            elif isinstance(entry, dict):
                value = entry.get("id")

                if not value:
                    continue

                if value.startswith("#"):
                    result.extend(
                        self.resolve_tag(
                            value[1:],
                            seen.copy(),
                        )
                    )
                else:
                    result.append(
                        plain_id(
                            normalize_id(value)
                        )
                    )

        # Preserve order while removing duplicates.
        return list(dict.fromkeys(result))


def parse_ingredient(
    value: Any,
    database: ResourceDatabase,
) -> List[str]:
    """
    Convert a Minecraft ingredient JSON object into every item
    that can satisfy it.

    Supported:

        "minecraft:iron_ingot"

        {"item": "minecraft:iron_ingot"}

        {"tag": "minecraft:planks"}

        [
            {"item": "..."},
            {"tag": "..."}
        ]
    """
    result: List[str] = []

    if isinstance(value, str):
        if value.startswith("#"):
            result.extend(
                database.resolve_tag(
                    value[1:]
                )
            )
        else:
            result.append(
                plain_id(
                    normalize_id(value)
                )
            )

    elif isinstance(value, list):
        for item in value:
            result.extend(
                parse_ingredient(
                    item,
                    database,
                )
            )

    elif isinstance(value, dict):
        if "item" in value:
            result.append(
                plain_id(
                    normalize_id(
                        value["item"]
                    )
                )
            )

        elif "tag" in value:
            result.extend(
                database.resolve_tag(
                    value["tag"]
                )
            )

    return list(dict.fromkeys(result))


def parse_output(
    data: Dict[str, Any],
) -> Tuple[str | None, int]:
    result = data.get("result")

    if isinstance(result, str):
        return (
            plain_id(
                normalize_id(result)
            ),
            int(data.get("count", 1)),
        )

    if isinstance(result, dict):
        item = result.get("item")

        if item:
            return (
                plain_id(
                    normalize_id(item)
                ),
                int(result.get("count", 1)),
            )

    # Newer recipe formats sometimes use "item".
    item = data.get("item")

    if item:
        return (
            plain_id(
                normalize_id(item)
            ),
            int(data.get("count", 1)),
        )

    return None, 0


def parse_recipe(
    recipe_id: str,
    data: Dict[str, Any],
    source: str,
) -> Recipe | None:
    recipe_type = data.get("type")

    if not isinstance(recipe_type, str):
        return None

    output, output_count = parse_output(data)

    # Smithing transform uses "result" differently in some versions.
    if recipe_type.endswith("smithing_transform"):
        result = data.get("result", {})

        if isinstance(result, dict):
            output = result.get("item")
            output_count = int(
                result.get("count", 1)
            )

            if output:
                output = plain_id(
                    normalize_id(output)
                )

    if not output:
        return None

    ingredients: List[List[str]] = []

    if recipe_type.endswith("crafting_shaped"):
        key = data.get("key", {})
        pattern = data.get("pattern", [])

        for row in pattern:
            for symbol in row:
                if symbol == " ":
                    continue

                ingredient = key.get(symbol)

                choices = parse_ingredient(
                    ingredient,
                    database=GLOBAL_DATABASE,
                )

                if choices:
                    ingredients.append(
                        choices
                    )

    elif recipe_type.endswith("crafting_shapeless"):
        for ingredient in data.get(
            "ingredients",
            [],
        ):
            choices = parse_ingredient(
                ingredient,
                database=GLOBAL_DATABASE,
            )

            if choices:
                ingredients.append(
                    choices
                )

    elif recipe_type.endswith("smelting"):
        choices = parse_ingredient(
            data.get("ingredient"),
            GLOBAL_DATABASE,
        )

        if choices:
            ingredients.append(choices)

    elif recipe_type.endswith("blasting"):
        choices = parse_ingredient(
            data.get("ingredient"),
            GLOBAL_DATABASE,
        )

        if choices:
            ingredients.append(choices)

    elif recipe_type.endswith("smoking"):
        choices = parse_ingredient(
            data.get("ingredient"),
            GLOBAL_DATABASE,
        )

        if choices:
            ingredients.append(choices)

    elif recipe_type.endswith("campfire_cooking"):
        choices = parse_ingredient(
            data.get("ingredient"),
            GLOBAL_DATABASE,
        )

        if choices:
            ingredients.append(choices)

    elif recipe_type.endswith("stonecutting"):
        choices = parse_ingredient(
            data.get("ingredient"),
            GLOBAL_DATABASE,
        )

        if choices:
            ingredients.append(choices)

    elif recipe_type.endswith("smithing_transform"):
        for key in (
            "template",
            "base",
            "addition",
        ):
            choices = parse_ingredient(
                data.get(key),
                GLOBAL_DATABASE,
            )

            if choices:
                ingredients.append(choices)

    elif recipe_type.endswith("smithing_trim"):
        for key in (
            "template",
            "base",
            "addition",
        ):
            choices = parse_ingredient(
                data.get(key),
                GLOBAL_DATABASE,
            )

            if choices:
                ingredients.append(choices)

        # Smithing trims do not have one fixed output item.
        # The resulting armor item depends on the base item,
        # so this extractor does not treat it as a normal fixed-output
        # recipe.

    else:
        return None

    return {
        "id": recipe_id,
        "type": recipe_type,
        "output": output,
        "output_count": output_count,
        "ingredients": ingredients,
        "source": source,
    }


def discover_database(
    archives: List[Tuple[str, Path]],
) -> List[Recipe]:
    global GLOBAL_DATABASE

    GLOBAL_DATABASE = ResourceDatabase()

    # First pass: collect tags.
    for source, path in archives:
        with zipfile.ZipFile(path) as archive:
            for name in tag_files_in_archive(archive):
                info = namespace_and_path(name)

                if info is None:
                    continue

                namespace, relative = info

                if not relative.startswith(
                    "tags/items/"
                ):
                    continue

                tag_path = relative[
                    len("tags/items/") :
                ]

                if tag_path.endswith(".json"):
                    tag_path = tag_path[:-5]

                tag_id = (
                    namespace
                    + ":"
                    + tag_path.replace(
                        "/",
                        "/",
                    )
                )

                data = read_json_from_zip(
                    archive,
                    name,
                )

                GLOBAL_DATABASE.add_tag(
                    tag_id,
                    data,
                )

    recipes: List[Recipe] = []

    # Second pass: recipes.
    for source, path in archives:
        with zipfile.ZipFile(path) as archive:
            for name in recipe_files_in_archive(
                archive
            ):
                info = namespace_and_path(name)

                if info is None:
                    continue

                namespace, relative = info

                recipe_path = relative[
                    len("recipes/") :
                ]

                if recipe_path.endswith(".json"):
                    recipe_path = recipe_path[:-5]

                recipe_id = (
                    namespace
                    + ":"
                    + recipe_path
                )

                try:
                    data = read_json_from_zip(
                        archive,
                        name,
                    )
                except Exception as exc:
                    print(
                        f"Warning: couldn't read {name}: {exc}",
                        file=sys.stderr,
                    )
                    continue

                recipe = parse_recipe(
                    recipe_id,
                    data,
                    source + "/" + name,
                )

                if recipe is not None:
                    recipes.append(recipe)

    recipes.sort(
        key=lambda recipe: (
            recipe["output"],
            recipe["id"],
        )
    )

    return recipes


GLOBAL_DATABASE = ResourceDatabase()


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Extract Minecraft recipes from jars/datapacks."
        )
    )

    parser.add_argument(
        "--minecraft",
        type=Path,
        help="Path to the Minecraft 1.20.1 client jar.",
    )

    parser.add_argument(
        "--mods-dir",
        type=Path,
        help="Directory containing mod jars.",
    )

    parser.add_argument(
        "--world",
        type=Path,
        help="Minecraft world directory.",
    )

    parser.add_argument(
        "--datapacks",
        type=Path,
        help="Additional datapack directory.",
    )

    parser.add_argument(
        "--out",
        type=Path,
        default=Path("recipes.json"),
        help="Output JSON file.",
    )

    args = parser.parse_args()

    archives = collect_archives(
        args.minecraft,
        args.mods_dir,
        args.world,
        args.datapacks,
    )

    if not archives:
        parser.error(
            "No Minecraft jar, mods, world datapacks, or datapacks were found."
        )

    print("Sources:")

    for source, path in archives:
        print(
            f"  {source}: {path}"
        )

    recipes = discover_database(
        archives
    )

    output = {
        "minecraft_recipe_database": 1,
        "recipe_count": len(recipes),
        "recipes": recipes,
    }

    args.out.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    args.out.write_text(
        json.dumps(
            output,
            indent=2,
        ),
        encoding="utf-8",
    )

    print()
    print(
        f"Found {len(recipes)} recipes."
    )
    print(
        f"Wrote {args.out}"
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(
        main()
    )