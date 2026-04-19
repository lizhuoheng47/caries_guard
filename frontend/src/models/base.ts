export interface PaginatedList<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}
