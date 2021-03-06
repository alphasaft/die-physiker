package loaders.base

class Ast : AstNode() {

    internal fun removeAllBranchesSafeFor(branches: Set<List<String>>) {
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

    override fun clean(): Ast {
        return super.clean().toAst()
    }

    override fun copy(): Ast {
        return super.copy().toAst()
    }
}
