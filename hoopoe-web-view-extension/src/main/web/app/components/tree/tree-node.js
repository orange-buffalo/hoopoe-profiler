let nodeId = 0;

class TreeNode {
  constructor(data) {
    this.data = data;
    this.expanded = false;
    this.children = [];
    this.id = nodeId++;
    this.$childrenProcessed = !this.data.children;
  }

  static of(data) {
    return data.map(dataElement => new TreeNode(dataElement));
  }

  isLeaf() {
    return this.children.length === 0
  }

  expand() {
    if (!this.$childrenProcessed) {
      this.children = this.data.children.map(childData => new TreeNode(childData));
      this.$childrenProcessed = true;
    }
    this.expanded = true;
  }

  collapse() {
    this.expanded = false;
  }

  toggle() {
    if (this.expanded) {
      this.collapse();
    }
    else {
      this.expand();
    }
  }
}

export default TreeNode;