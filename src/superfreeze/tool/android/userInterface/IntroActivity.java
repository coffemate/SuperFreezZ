/*
Copyright (c) 2018 Hocuri

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

/**
 * Shows the intro slides on the very first startup
 */
public final class IntroActivity extends AppIntro {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SliderPage page1 = new SliderPage();
		page1.setTitle(getString(R.string.welcome));
		page1.setDescription(getString(R.string.short_description));
		page1.setBgColor(getResources().getColor(R.color.ic_launcher_background));
		addSlide(AppIntroFragment.newInstance(page1));

		SliderPage page2 = new SliderPage();
		page2.setTitle(getString(R.string.intro_always_freeze_title));
		page2.setDescription(getString(R.string.intro_always_freeze_explanation));
		page2.setImageDrawable(R.drawable.symbol_always_freeze);
		page2.setBgColor(getResources().getColor(R.color.always_freeeze_background));
		addSlide(AppIntroFragment.newInstance(page2));

		SliderPage page3 = new SliderPage();
		page3.setTitle(getString(R.string.intro_never_freeze_title));
		page3.setDescription(getString(R.string.intro_never_freeze_explanation));
		page3.setImageDrawable(R.drawable.symbol_never_freeze);
		page3.setBgColor(getResources().getColor(R.color.never_freeeze_background));
		addSlide(AppIntroFragment.newInstance(page3));

		SliderPage page4 = new SliderPage();
		page4.setTitle(getString(R.string.intro_auto_freeze_title));
		page4.setDescription(getString(R.string.intro_auto_freeze_explanation));
		page4.setImageDrawable(R.drawable.symbol_freeze_when_inactive);
		page4.setBgColor(getResources().getColor(R.color.inactive_freeeze_background));
		addSlide(AppIntroFragment.newInstance(page4));

		addSlide(new AccessibilityServiceChooserFragment());

		setDepthAnimation();
		showSkipButton(false);
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

}
