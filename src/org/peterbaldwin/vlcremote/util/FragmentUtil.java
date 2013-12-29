/*
 * Copyright (C) 2013 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.util;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

/**
 *
 * @author Sam Malone
 */
public class FragmentUtil {
    
    private final FragmentManager fm;

    public FragmentUtil(FragmentManager fm) {
        this.fm = fm;
    }
    
    public void removeFragmentsByTag(String... tags) {
        FragmentTransaction ft = fm.beginTransaction();
        for(String tag : tags) {
            Fragment f = fm.findFragmentByTag(tag);
            if(f != null) {
                ft.remove(f);
            }
        }
        if(!ft.isEmpty()) {
            ft.commit();
            fm.executePendingTransactions();
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Fragment> T findOrAddFragment(String tag, Class<T> fragmentClass) {
        try {
            T fragment = (T) fm.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = fragmentClass.newInstance();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.add(fragment, tag);
                fragmentTransaction.commit();
                fm.executePendingTransactions();
            }
            return fragment;
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Fragment> T findOrReplaceOptionalFragment(Activity activity, int res, String tag, Class<T> fragmentClass) {
        if(activity.findViewById(res) != null) {
            return findOrReplaceFragment(res, tag, fragmentClass);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Fragment> T findOrReplaceOptionalFragment(View view, int res, String tag, Class<T> fragmentClass) {
        if(view.findViewById(res) != null) {
            return findOrReplaceFragment(res, tag, fragmentClass);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Fragment> T findOrReplaceFragment(int res, String tag, Class<T> fragmentClass) {
        try {
            T fragment = (T) fm.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = fragmentClass.newInstance();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(res, fragment, tag);
                fragmentTransaction.commit();
                fm.executePendingTransactions();
            }
            return fragment;
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
}
