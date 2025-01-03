/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.stario.launcher.ui.widgets;

import androidx.annotation.Nullable;

import com.stario.launcher.sheet.widgets.WidgetSize;
import com.stario.launcher.ui.Measurements;

import java.util.HashSet;
import java.util.Set;

public class WidgetMap {
    private final Set<Cell> set;

    WidgetMap() {
        this.set = new HashSet<>();
    }

    void add(Cell origin, WidgetSize size) {
        for (int row = origin.row; row < origin.row + size.height; row++) {
            for (int column = origin.column; column < origin.column + size.width; column++) {
                set.add(new Cell(row, column));
            }
        }
    }

    Cell getAvailableOrigin(WidgetSize size) {
        int column = 0;
        int row = 0;

        Cell testedCell;

        do {
            testedCell = new Cell(row, column);

            column = column + 1;

            if (column >= Measurements.getWidgetColumnCount()) {
                column = 0;
                row++;
            }
        } while (!checkFreeSpace(testedCell, size));

        return testedCell;
    }


    private boolean checkFreeSpace(Cell origin, WidgetSize size) {
        if (origin.column + size.width > Measurements.getWidgetColumnCount()) {
            return false;
        }

        for (int row = origin.row; row < origin.row + size.height; row++) {
            for (int column = origin.column; column < origin.column + size.width; column++) {
                if (set.contains(new Cell(row, column))) {
                    return false;
                }
            }
        }

        return true;
    }

    void clear() {
        set.clear();
    }

    static class Cell {
        public final int row;
        public final int column;

        public Cell(int row, int column) {
            this.row = row;
            this.column = column;
        }

        @Override
        public int hashCode() {
            int result = row;
            result = 31 * result + column;

            return result;
        }

        @Override
        public boolean equals(@Nullable Object object) {
            return object instanceof Cell &&
                    ((Cell) object).row == row &&
                    ((Cell) object).column == column;
        }
    }
}
