class HeroModel {
  constructor(message, hasError, buttonText) {
    this.message = message;
    this.hasError = hasError;
    this.buttonText = buttonText;
  }

  static error() {
    return new HeroModel(null, true, null);
  }

  static forMessage(message) {
    return new HeroModel(message, false, null);
  }

  static empty() {
    return new HeroModel(null, false,  null);
  }

  withButton(buttonText) {
    this.buttonText = buttonText;
    return this;
  }

  isVisible() {
    return this.hasError || this.message;
  }

}

export default HeroModel;
