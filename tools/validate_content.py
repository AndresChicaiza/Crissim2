#!/usr/bin/env python3
"""Valida escenarios de CrisisSim (formato snake_case): esquema JSON + integridad.

Uso:  python tools/validate_content.py [--strict] [archivos.json ...]
  --strict  trata los avisos (p. ej. protocolos [PENDIENTE]) como errores.
Opcional: pip install jsonschema  (activa la validación contra tools/schema).
"""
import argparse
import json
import sys
from pathlib import Path

try:
    from jsonschema import Draft7Validator
except ImportError:  # el validador semántico funciona igual sin la librería
    Draft7Validator = None

ROOT = Path(__file__).resolve().parent.parent
SCHEMA = ROOT / "tools" / "schema" / "scenario.schema.json"
SCENARIOS = ROOT / "app" / "src" / "main" / "assets" / "content" / "scenarios"


def semantic_checks(sc):
    errors, warnings = [], []
    nodes = {}
    for n in sc["nodes"]:
        if n["id"] in nodes:
            errors.append(f"nodo duplicado: {n['id']}")
        nodes[n["id"]] = n

    if sc["start_node"] not in nodes:
        return [f"start_node inexistente: {sc['start_node']}"], warnings
    if nodes[sc["start_node"]].get("is_end"):
        errors.append("start_node no puede ser un nodo final")
    if not 0 <= sc["passing_score"] <= 100:
        errors.append("passing_score es un PORCENTAJE (0-100)")

    edges = {}
    for n in nodes.values():
        opts = n.get("options", [])
        ids = [o["id"] for o in opts]
        if len(ids) != len(set(ids)):
            errors.append(f"{n['id']}: ids de opción repetidos")
        if n.get("is_end"):
            if opts:
                errors.append(f"{n['id']}: un nodo final no debe tener opciones")
            if "end_type" not in n:
                errors.append(f"{n['id']}: nodo final sin end_type")
            edges[n["id"]] = []
            continue
        if len(opts) < 2:
            errors.append(f"{n['id']}: necesita al menos 2 opciones")
        if not any(o.get("feedback", {}).get("is_correct") for o in opts):
            errors.append(f"{n['id']}: no tiene ninguna opción correcta")
        has_limit = n.get("time_limit_seconds") is not None
        idx = n.get("default_option_index")
        if has_limit and idx is None:
            errors.append(f"{n['id']}: time_limit_seconds requiere default_option_index")
        if idx is not None:
            if not 0 <= idx < len(opts):
                errors.append(f"{n['id']}: default_option_index {idx} fuera de rango")
            elif opts[idx].get("feedback", {}).get("is_correct"):
                errors.append(f"{n['id']}: el timeout aplica la opción CORRECTA (esperar daría el máximo)")
        for o in opts:
            if o["next_node"] not in nodes:
                errors.append(f"{n['id']}/{o['id']}: next_node inexistente '{o['next_node']}'")
            fb = o.get("feedback", {})
            if "[PENDIENTE" in str(fb.get("protocol_ref", "")):
                warnings.append(f"{n['id']}/{o['id']}: protocolo sin citar [PENDIENTE]")
        edges[n["id"]] = [o["next_node"] for o in opts if o["next_node"] in nodes]

    seen, stack = set(), [sc["start_node"]]
    while stack:
        cur = stack.pop()
        if cur not in seen:
            seen.add(cur)
            stack.extend(edges.get(cur, []))
    for nid in nodes:
        if nid not in seen:
            errors.append(f"nodo inalcanzable: {nid}")

    color = {nid: 0 for nid in nodes}

    def dfs(u):
        color[u] = 1
        for v in edges.get(u, []):
            if color[v] == 1:
                errors.append(f"ciclo detectado: {u} -> {v}")
            elif color[v] == 0:
                dfs(v)
        color[u] = 2

    dfs(sc["start_node"])

    if not any(nodes[i].get("end_type") == "success" for i in seen if nodes[i].get("is_end")):
        errors.append("no hay un final 'success' alcanzable")
    return errors, warnings


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="*", type=Path)
    ap.add_argument("--strict", action="store_true")
    args = ap.parse_args()

    files = args.files or sorted(SCENARIOS.glob("*.json"))
    if not files:
        print(f"No se encontraron escenarios en {SCENARIOS}")
        return 1

    validator = None
    if Draft7Validator is not None:
        validator = Draft7Validator(json.loads(SCHEMA.read_text(encoding="utf-8")))
    else:
        print("(aviso) jsonschema no instalado: solo se hacen las revisiones de integridad")

    failed, ids = False, {}
    for f in files:
        data = json.loads(f.read_text(encoding="utf-8"))
        errors = []
        if validator:
            errors = [
                f"{'/'.join(map(str, e.absolute_path)) or '(raíz)'}: {e.message}"
                for e in sorted(validator.iter_errors(data), key=lambda e: list(map(str, e.absolute_path)))
            ]
        warnings = []
        if not errors:
            errors, warnings = semantic_checks(data)
            if data["id"] in ids:
                errors.append(f"id de escenario repetido (también en {ids[data['id']]})")
            ids[data["id"]] = f.name
        if args.strict:
            errors, warnings = errors + warnings, []
        print(f"[{'ERROR' if errors else 'OK'}] {f.name}")
        for m in errors:
            print(f"   error: {m}")
        for m in warnings:
            print(f"   aviso: {m}")
        failed = failed or bool(errors)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
