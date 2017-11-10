class ProfiledInvocations {

  constructor(apiResponse) {
    console.log(apiResponse);
    if (apiResponse && apiResponse.invocations && apiResponse.invocations.length) {
      this.roots = apiResponse.invocations;
    }
    else {
      this.roots = [];
    }
  }

  static empty() {
    return new ProfiledInvocations(null);
  }

  isEmpty() {
    return this.roots.length === 0;
  }

}

export default ProfiledInvocations;