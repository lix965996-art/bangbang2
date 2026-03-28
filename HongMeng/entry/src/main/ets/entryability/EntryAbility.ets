
import UIAbility from '@ohos.app.ability.UIAbility';
import hilog from '@ohos.hilog';
import window from '@ohos.window';
import Want from '@ohos.app.ability.Want';
import AbilityConstant from '@ohos.app.ability.AbilityConstant';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam) {
    hilog.info(0x0000, 'SmartFarm', 'EntryAbility onCreate');
  }

  onDestroy() {
    hilog.info(0x0000, 'SmartFarm', 'EntryAbility onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage) {
    hilog.info(0x0000, 'SmartFarm', 'EntryAbility onWindowStageCreate');

    windowStage.loadContent('pages/LoginPage', (err, data) => {
      if (err.code) {
        hilog.error(0x0000, 'SmartFarm', 'Failed to load content. Cause: %{public}s', JSON.stringify(err));
        return;
      }
      hilog.info(0x0000, 'SmartFarm', 'Succeeded in loading content. Data: %{public}s', JSON.stringify(data));
    });
  }

  onWindowStageDestroy() {
    hilog.info(0x0000, 'SmartFarm', 'EntryAbility onWindowStageDestroy');
  }

  onForeground() {
    hilog.info(0x0000, 'SmartFarm', 'EntryAbility onForeground');
  }

  onBackground() {
    hilog.info(0x0000, 'SmartFarm', 'EntryAbility onBackground');
  }
}
