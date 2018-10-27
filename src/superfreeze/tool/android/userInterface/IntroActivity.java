/*
Copyright (c) 2018 Hocceruser

This file is part of SuperFreezZ.

SuperFreezZ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreezZ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreezZ.  If not, see <http://www.gnu.org/licenses/>.
*/


package superfreeze.tool.android.userInterface;

import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import androidx.fragment.app.Fragment;
import superfreeze.tool.android.R;
import superfreeze.tool.android.database.DatabaseKt;

public final class IntroActivity extends AppIntro {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SliderPage page1 = new SliderPage();
		page1.setTitle("Welcome to SuperFreezZ!");
		page1.setDescription("SuperFreezZ makes it easy to entirely freeze all background activities of apps.");
		page1.setBgColor(getResources().getColor(R.color.ic_launcher_background));
		addSlide(AppIntroFragment.newInstance(page1));

		SliderPage page2 = new SliderPage();
		page2.setTitle("Freeze Mode: ALWAYS FREEZE");
		page2.setDescription("There are 3 freeze modes. You can choose one for every app. ALWAYS FREEZE will freeze an app in any case.");
		page2.setImageDrawable(R.drawable.symbol_always_freeze);
		page2.setBgColor(getResources().getColor(R.color.always_freeeze_background));
		addSlide(AppIntroFragment.newInstance(page2));

		SliderPage page3 = new SliderPage();
		page3.setTitle("Freeze Mode: NEVER FREEZE");
		page3.setDescription("NEVER FREEZE will - turn off freezing and never freeze an app.");
		page3.setImageDrawable(R.drawable.symbol_never_freeze);
		page3.setBgColor(getResources().getColor(R.color.never_freeeze_background));
		addSlide(AppIntroFragment.newInstance(page3));

		SliderPage page4 = new SliderPage();
		page4.setTitle("Freeze Mode: AUTO FREEZE");
		page4.setDescription("AUTO FREEZE will freeze an app only if it has not been used for some time (7 days by default)");
		page4.setImageDrawable(R.drawable.symbol_freeze_when_inactive);
		page4.setBgColor(getResources().getColor(R.color.inactive_freeeze_background));
		addSlide(AppIntroFragment.newInstance(page4));

		addSlide(new AccessibilityServiceChooserFragment());

		setDepthAnimation();
		showSkipButton(false);
		instance = this;
	}

	@Override
	public void onDonePressed(Fragment currentFragment) {
		super.onDonePressed(currentFragment);
		done();
	}

	public void done() {
		DatabaseKt.firstLaunchCompleted(getApplicationContext());
		finish();
	}

	static IntroActivity instance = null;
}
