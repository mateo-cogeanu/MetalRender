#!/usr/bin/env python3
"""Audit a live run; compiling a final pass alone is never full-stack success."""
import argparse
import hashlib
import json
from pathlib import Path
import re


def audit(run, reference):
    log = (run / 'logs/latest.log').read_text(errors='replace')
    compiled = sorted(set(re.findall(r'Compiled Vulkan custom pass (\S+) for pipeline', log)))
    bound = sorted(set(re.findall(r'Bound native Vulkan screen pass (\S+) with', log)))
    failures = sorted(set(line.split('(Iris) ', 1)[-1] for line in log.splitlines() if any(
        marker in line for marker in ('Failed to compile Vulkan override',
        'Skipping native Vulkan screen pass', 'Disabling Iris native Vulkan final pass',
        'Using a fully-lit fallback', 'Unknown variable:', 'VK_ERROR_INITIALIZATION_FAILED'))))
    missing = sorted(set(re.findall(r'\(Iris\)\s+(\S+)=(?:PLANNED|UNSUPPORTED) keys=', log)))
    changed = []
    checked = {}
    inputs = [reference / 'options.txt', reference / 'config/iris.properties']
    inputs += sorted((reference / 'shaderpacks').rglob('*'))
    for source in inputs:
        if not source.is_file():
            if source in inputs[:2]:
                changed.append(str(source.relative_to(reference)) + ' (reference missing)')
            continue
        relative = source.relative_to(reference)
        target = run / relative
        digest = hashlib.sha256(source.read_bytes()).hexdigest()
        checked[str(relative)] = digest
        if not target.is_file() or hashlib.sha256(target.read_bytes()).hexdigest() != digest:
            changed.append(str(relative))
    native = 'Native Metal bootstrap and command self-test passed' in log
    metal = 'Using graphics backend Vulkan' in log and 'MoltenVK' in log
    final = 'final/final' in bound
    incomplete = bool(failures or missing or changed or not native or not metal or not final)
    return dict(status='incomplete' if incomplete else 'requires_visual_validation',
        native_self_test=native, metal_backend=metal, final_pass_bound=final,
        compiled_passes=compiled, bound_passes=bound, missing_routes=missing,
        failures=failures, changed_inputs=changed, reference_sha256=checked,
        performance_comparison_valid=False)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('run', type=Path)
    parser.add_argument('--reference', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.run, args.reference)
    print(json.dumps(result, indent=2))
    raise SystemExit(1 if result['status'] == 'incomplete' else 0)
