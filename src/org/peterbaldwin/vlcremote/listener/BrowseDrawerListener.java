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

package org.peterbaldwin.vlcremote.listener;

import android.app.Activity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SlidingDrawer;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.fragment.BrowseFragment;

public final class BrowseDrawerListener implements SlidingDrawer.OnDrawerOpenListener,
        SlidingDrawer.OnDrawerCloseListener {

    private final Activity mActivity;
    private final SlidingDrawer mDrawer;
    private final BrowseFragment mBrowse;
    private ActionMode mActionMode;

    public BrowseDrawerListener(Activity activity, SlidingDrawer drawer, BrowseFragment browse) {
        mActivity = activity;
        mDrawer = drawer;
        mBrowse = browse;
    }

    /** {@inheritDoc} */
    public void onDrawerOpened() {
        startActionMode();
        mDrawer.getHandle().setVisibility(View.INVISIBLE);
    }

    /** {@inheritDoc} */
    public void onDrawerClosed() {
        finishActionMode();
        mDrawer.getHandle().setVisibility(View.VISIBLE);
    }

    private void startActionMode() {
        if (mActionMode == null) {
            mActionMode = mActivity.startActionMode(new ActionModeCallback());
        }
    }

    private void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }

    // BrowseDrawerListener can't be referenced by code running
    // on Android 1.6 if it implements ActionMode.Callback directly,
    // so implement ActionMode.Callback with a nested class instead.
    private class ActionModeCallback implements ActionMode.Callback {

        /** {@inheritDoc} */
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mBrowse.onOptionsItemSelected(item);
        }

        /** {@inheritDoc} */
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActivity.getMenuInflater().inflate(R.menu.browse_options, menu);
            return true;
        }

        /** {@inheritDoc} */
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        /** {@inheritDoc} */
        public void onDestroyActionMode(ActionMode mode) {
            if (mode == mActionMode) {
                if (mDrawer.isOpened()) {
                    mDrawer.animateClose();
                }
                mActionMode = null;
            }
        }
    }
}
