export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId?: string;
  timestamp?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}
