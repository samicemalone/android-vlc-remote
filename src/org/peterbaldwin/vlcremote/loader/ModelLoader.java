/*-
 *  Copyright (C) 2011 Peter Baldwin   
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

abstract class ModelLoader<D> extends AsyncTaskLoader<D> {

    private D mModel;

    protected ModelLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        if (mModel != null) {
            deliverResult(mModel);
        } else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(D model) {
        if (!isReset()) {
            super.deliverResult(model);
            mModel = model;
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        cancelLoad();
        mModel = null;
    }
}
