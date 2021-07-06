package loaders.base

class Ast(source: String) : AstNode() {
    init {
        content = source
    }

    fun removeAllBranchesSafeFor(branches: Set<List<String>>) {
        requireUnlocked()
        for (branch in this.branches.sortedByDescending { it.size }) {
            if (branch !in branches) {
                cutBranch(branch)
            }
        }

        val branchesCopy = branches.toList()
        branchesStorage.clear()
        branchesStorage.addAll(branchesCopy)
    }
}
