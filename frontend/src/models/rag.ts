export interface Citation {
  id: string;
  title: string;
  text: string;
  page?: number;
}

export interface RagResponse {
  session: string;
  request: string;
  text: string;
  citations: Citation[];
  version: string;
  safetyWarning: boolean;
  safetyMessages: string[];
  confidence?: number;
}
