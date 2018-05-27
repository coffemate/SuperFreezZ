package superfreeze.tool.android;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {
	private AppsListAdapter appsListAdapter;

	private ProgressBar progressBar;
	private PermissionResolver permissionResolver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

		RecyclerView listView = (RecyclerView)findViewById(android.R.id.list);

		appsListAdapter = new AppsListAdapter(this);
		listView.setLayoutManager(new LinearLayoutManager(this));
		listView.setAdapter(appsListAdapter);

		progressBar = (ProgressBar) findViewById(android.R.id.progress);
		progressBar.setVisibility(View.VISIBLE);

		new Loader(this, getApplicationContext()).execute();
		
		permissionResolver = new PermissionResolver(this);
	}



	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (!permissionResolver.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	public void hideProgressBar() {
		progressBar.setVisibility(View.GONE);
	}

	public void addItem(PackageInfo item) {
		appsListAdapter.addItem(item);
	}

    /**
     * The method that is responsible for showing the search icon in the top right hand corner.
     * @param menu The Menu to which the search icon is added.
     * @return
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
		final SearchView searchView = (SearchView)MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		assert searchManager != null;
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			@SuppressLint("RestrictedApi")//I guess that this is necessary because of a bug in the build tools
			public void onFocusChange(View view, boolean queryTextFocused) {
				if (!queryTextFocused && searchView.getQuery().length() < 1) {
					ActionBar supportActionBar = getSupportActionBar();
					assert supportActionBar != null;
					supportActionBar.collapseActionView();
				}
			}
		});
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String s) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String s) {
				appsListAdapter.setSearchPattern(s);
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}


}
