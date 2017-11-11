let nodeId = 0;

class TreeNode {
  constructor(data) {
    this.data = data;
    this.expanded = false;
    this.children = [];
    this.id = nodeId++;
    this.$childrenProcessed = !this.data.children;
    this.selected = false;
  }

  static of(data) {
    return data.map(dataElement => new TreeNode(dataElement));
  }

  isLeaf() {
    return !this.data.children || this.data.children.length === 0
  }

  expand() {
    if (!this.$childrenProcessed) {
      this.children = this.data.children.map(childData => new TreeNode(childData));
      this.$childrenProcessed = true;
    }
    this.expanded = true;

    if (this.children.length === 1) {
      this.children.forEach(childNode => childNode.expand())
    }
  }

  collapse() {
    this.expanded = false;
    if (this.$childrenProcessed) {
      this.children.forEach(childNode => childNode.collapse())
    }
  }

  toggle() {
    if (this.expanded) {
      this.collapse();
    }
    else {
      this.expand();
    }
  }

  toggleSelection() {
    this.selected = !this.selected;
  }
}

export default TreeNode;