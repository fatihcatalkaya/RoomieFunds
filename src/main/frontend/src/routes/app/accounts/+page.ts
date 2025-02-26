import { getApiAccount } from "$lib/client";
import type { PageLoad } from "./$types";

function buildTree(accounts: Account[]): TreeNode[] {
    const rootNodes: TreeNode[] = [];

    // Function to insert an account into the tree
    const insertToTree = (path: string[], account: Account, node: TreeNode[]) => {
        const segment = path.shift();
        if (!segment) return;

        // Find if the node with the same name already exists
        let currentNode = node.find((n) => n.name === segment) as TreeNode | undefined;

        if (!currentNode) {
            // If the node doesn't exist, create a new one
            currentNode = { name: segment, children: [] };
            node.push(currentNode);
        }

        // If there are still parts of the path, continue building the tree
        if (path.length > 1) {
            insertToTree(path, account, currentNode.children as any);
        } else {
            // When we reach the last segment, insert the account as a leaf node
            // Instead of pushing an Account directly, we push a TreeNode with children
            currentNode.children.push(account);
        }
    };

    // Iterate through all accounts and insert them into the tree
    accounts.forEach((account) => {
        const path = account.name!.split(':');
        insertToTree(path, account, rootNodes);
    });

    return rootNodes;
}

export const load: PageLoad = () => {
    let accountsQuery = async () => {
		let accountQuery = await getApiAccount();

		if (accountQuery.error) {
			throw accountQuery.error;
		} else {
			return buildTree(accountQuery.data!);
		}
	};

    return {
        accountsQuery: accountsQuery()
    }
};