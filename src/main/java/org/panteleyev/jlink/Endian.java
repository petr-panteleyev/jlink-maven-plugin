// Copyright © 2024-2026 Petr Panteleyev
// SPDX-License-Identifier: BSD-2-Clause
package org.panteleyev.jlink;

public enum Endian {
    LITTLE,
    BIG;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
