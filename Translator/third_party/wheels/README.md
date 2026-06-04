# Offline Wheels

This folder stores small Python wheels that can be installed without internet:

```powershell
python scripts\install_offline_packages.py
```

Current wheelhouse target:

```text
requirements-training-lite.txt
```

The current workstation already has the heavy neural-training packages
installed:

```text
torch
transformers
sentencepiece
datasets
accelerate
```

CUDA-enabled `torch` wheels are very large and depend on the exact Python and
CUDA version. Keep them in a separate local archive if this project must be
moved to another offline computer.

