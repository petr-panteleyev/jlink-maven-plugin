/*
 Copyright © 2024 Petr Panteleyev <petr@panteleyev.org>
 SPDX-License-Identifier: BSD-2-Clause
 */
package org.panteleyev.jlink;

public enum Endian implements EnumParameter {
    LITTLE,
    BIG;

    @Override
    public String getValue() {
        return name().toLowerCase();
    }
}
