class HpDialogModel {
  constructor() {
    this.visible = false;
  }

  show() {
    this.visible = true;
  }

  hide() {
    this.visible = false;
  }

  $onKeyDown(event) {
    // noinspection EqualityComparisonWithCoercionJS
    if (event.keyCode == 27) {
      this.visible = false;
    }
  }
}

export default HpDialogModel;