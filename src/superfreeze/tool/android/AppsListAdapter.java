/*
Copyright (c) 2015 axxapy
Copyright (c) 2018 Hocceruser

This file is part of SuperFreeze.

SuperFreeze is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreeze is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreeze.  If not, see <http://www.gnu.org/licenses/>.
SuperFreeze is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreeze is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreeze.  If not, see <http://www.gnu.org/licenses/>.
*/


package superfreeze.tool.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This class is responsible for viewing the list of installed apps.
 */
public class AppsListAdapter extends RecyclerView.Adapter<AppsListAdapter.ViewHolder> {
	private ThreadFactory tFactory = new ThreadFactory() {
		@Override
		public Thread newThread(@NonNull Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	};

	/**
	 * This list contains the apps that are shown to the user. When the user searches, this is a subset of list_original.
	 */
	private ArrayList<PackageInfo> list                 = new ArrayList<PackageInfo>();

	/**
	 * This list contains all user apps.
	 */
	private ArrayList<PackageInfo> list_original        = new ArrayList<PackageInfo>();

	private ExecutorService        executorServiceNames = Executors.newFixedThreadPool(3, tFactory);
	private ExecutorService        executorServiceIcons = Executors.newFixedThreadPool(3, tFactory);
	private Handler                handler              = new Handler();
	private MainActivity           mActivity;
	private final PackageManager   packageManager;

	private int names_to_load = 0;

	private Map<String, String>   cache_appName = Collections.synchronizedMap(new LinkedHashMap<String, String>(10, 1.5f, true));
	private Map<String, Drawable> cache_appIcon = Collections.synchronizedMap(new LinkedHashMap<String, Drawable>(10, 1.5f, true));

	private String search_pattern;

	AppsListAdapter(MainActivity activity) {
		this.packageManager = activity.getPackageManager();
		mActivity = activity;
	}

	class AppNameLoader implements Runnable {
		private PackageInfo package_info;

		AppNameLoader(PackageInfo info) {
			package_info = info;
		}

		@Override
		public void run() {
			cache_appName.put(package_info.packageName, (String) package_info.applicationInfo.loadLabel(packageManager));
			handler.post(new Runnable() {
				@Override
				public void run() {
					names_to_load--;
					if (names_to_load == 0) {
						mActivity.hideProgressBar();
						executorServiceNames.shutdown();
					}
				}
			});
		}
	}

	class GuiLoader implements Runnable {
		private ViewHolder  viewHolder;
		private PackageInfo package_info;

		GuiLoader(ViewHolder h, PackageInfo info) {
			viewHolder = h;
			package_info = info;
		}

		@Override
		public void run() {
			boolean first = true;
			do {
				try {
					final String appName = cache_appName.containsKey(package_info.packageName)
						? cache_appName.get(package_info.packageName)
						: (String) package_info.applicationInfo.loadLabel(packageManager);
					final Drawable icon = package_info.applicationInfo.loadIcon(packageManager);
					cache_appName.put(package_info.packageName, appName);
					cache_appIcon.put(package_info.packageName, icon);
					handler.post(new Runnable() {
						@Override
						public void run() {
							viewHolder.setAppName(appName, search_pattern);
							viewHolder.imgIcon.setImageDrawable(icon);
						}
					});


				} catch (OutOfMemoryError ex) {
					cache_appIcon.clear();
					cache_appName.clear();
					if (first) {
						first = false;
						continue;
					}
				}
				break;
			} while (true);
		}
	}

	class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
		private TextView       txtAppName;
		public  ImageView      imgIcon;
		private Context        context;

		ViewHolder(View v, Context context) {
			super(v);
			imgIcon = (ImageView) v.findViewById(R.id.imgIcon);
			txtAppName = (TextView) v.findViewById(R.id.txtAppName);
			v.setOnClickListener(this);
			this.context = context;
		}

		/**
		 * This method defines what is done when a list item (that is, an app) is clicked.
		 * @param v The clicked view.
		 */
		@Override
		public void onClick(View v) {
			FreezerKt.freezeApp(getItem(getAdapterPosition()).packageName, context);

			//Remove it from the list after a delay of 500ms
			// (if it is removed faster, it will disappear while the list is still shown)
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					list.remove(getAdapterPosition());
					notifyDataSetChanged();
				}
			}, 500);
		}

		public void setAppName(String name, String highlight) {
			setAndHighlight(txtAppName, name, highlight);
		}

		private void setAndHighlight(TextView view, String value, String pattern) {
			view.setText(value);
			if (pattern == null || pattern.isEmpty()) return;// nothing to highlight

			value = value.toLowerCase();
			for (int offset = 0, index = value.indexOf(pattern, offset);
			     index >= 0 && offset < value.length();
			     index = value.indexOf(pattern, offset)) {
				Spannable span = new SpannableString(view.getText());
				span.setSpan(new ForegroundColorSpan(Color.BLUE), index, index + pattern.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				view.setText(span);
				offset += index + pattern.length();
			}
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		return new ViewHolder(
				LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false),
				viewGroup.getContext());
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int i) {
		PackageInfo item = list.get(i);
		if (cache_appIcon.containsKey(item.packageName) && cache_appName.containsKey(item.packageName)) {
			holder.setAppName(cache_appName.get(item.packageName), search_pattern);
			holder.imgIcon.setImageDrawable(cache_appIcon.get(item.packageName));
		} else {
			holder.setAppName(item.packageName, search_pattern);
			holder.imgIcon.setImageDrawable(null);
			executorServiceIcons.submit(new GuiLoader(holder, item));
		}
	}

	@Contract(pure = true)
	private PackageInfo getItem(int pos) {
		return list.get(pos);
	}

	@Override
	public int getItemCount() {
		return list.size();
	}

	public void addItem(PackageInfo item) {
		names_to_load++;
		executorServiceNames.submit(new AppNameLoader(item));
		list_original.add(item);
		if (isMatchedBySearchPattern(item)) {
			list.add(item);
		}
		notifyDataSetChanged();
	}

	public void setSearchPattern(String pattern) {
		search_pattern = pattern.toLowerCase();
		filterListByPattern();
	}


	/**
	 * Filters the apps list by the search pattern the user typed in.
	 */
	private void filterListByPattern() {
		list.clear();
		for (PackageInfo info : list_original) {

			if (isMatchedBySearchPattern(info)){
				list.add(info);
			}
		}
		notifyDataSetChanged();
	}

	/**
	 * Returns true if the the app name contains the search pattern.
	 * @param info The PackageInfo describing the package.
	 * @return Whether the the app name contains the search pattern.
	 */
	private boolean isMatchedBySearchPattern(PackageInfo info) {

		if (search_pattern == null || search_pattern.isEmpty()) {
			return true;// empty search pattern: Show all apps
		} else if (cache_appName.containsKey(info.packageName) && cache_appName.get(info.packageName).toLowerCase().contains(search_pattern)) {
			return true;// search in application name
		}

		return false;
	}
}