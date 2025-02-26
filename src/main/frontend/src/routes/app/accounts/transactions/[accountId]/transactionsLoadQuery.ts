import { getApiAccountByAccountId, getApiTransactionAccountByAccountId } from "$lib/client";

export function createFetchQuery(accountId: number) {
    const transactionQuery = async () => {
        let query = await getApiTransactionAccountByAccountId({
            path: {
                accountId: accountId, // if this throws we catch this in the ui
            }
        });

        await new Promise(resolve => setTimeout(resolve, 1000));

        if (query.error) {
            throw query.error;
        } else {
            return query.data!;
        }
    };

    const accountQuery = async () => {
        let query = await getApiAccountByAccountId({
            path: {
                accountId: accountId, // if this throws we catch this in the ui
            }
        });

        if (query.error) {
            throw query.error;
        } else {
            return query.data!;
        }
    }

    return Promise.all([transactionQuery(), accountQuery()])
}