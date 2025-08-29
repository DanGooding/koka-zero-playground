# SecComp Profile which permits running Bubblewrap

This is based on docker's default seccomp profile for an amd64 machine with CAP_SYS_ADMIN. Additionally `pivot_root` is
permitted - this is used by `bwrap` to set up the sandbox.

This file is generated as a golden-file in DanGooding/runtime-tools/generate/testdata/bubblewrap-profile.json.golden