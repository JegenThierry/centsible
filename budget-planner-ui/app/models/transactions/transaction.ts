import type {Category} from "~/models/category/category";

export interface Transaction {
    id: string,
    amount: number,
    category: Category,
    description: string,
    transactionDate: string,
    createdAt: string,
    updatedAt: string,
}