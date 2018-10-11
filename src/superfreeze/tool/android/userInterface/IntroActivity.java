package superfreeze.tool.android.userInterface;

import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import superfreeze.tool.android.R;

public final class IntroActivity extends AppIntro {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SliderPage page1 = new SliderPage();
		page1.setTitle("Welcome to SuperFreezZ!");
		page1.setDescription("SuperFreezZ makes it easy to entirely freeze all background activities of apps.");
		page1.setImageDrawable(R.mipmap.ic_launcher);
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

		setFadeAnimation();
	}
}
