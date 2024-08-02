/*
    Copyright (C) 2015 The Android Open Source Project
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

package com.stario.launcher.sheet.behavior;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.customview.view.AbsSavedState;

class SavedSheetState extends AbsSavedState {
    @SheetBehavior.State
    final int state;

    public SavedSheetState(@NonNull Parcel source, ClassLoader loader) {
        super(source, loader);

        state = source.readInt();
    }

    public SavedSheetState(Parcelable superState, @SheetBehavior.State int state) {
        super(superState);
        this.state = state;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        super.writeToParcel(out, flags);

        out.writeInt(state);
    }

    public static final Creator<SavedSheetState> CREATOR =
            new ClassLoaderCreator<>() {
                @NonNull
                @Override
                public SavedSheetState createFromParcel(@NonNull Parcel in, ClassLoader loader) {
                    return new SavedSheetState(in, loader);
                }

                @NonNull
                @Override
                public SavedSheetState createFromParcel(@NonNull Parcel in) {
                    return new SavedSheetState(in, null);
                }

                @NonNull
                @Override
                public SavedSheetState[] newArray(int size) {
                    return new SavedSheetState[size];
                }
            };
}
