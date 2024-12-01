from typing import List, Dict
from duckduckgo_search import ddg
from langchain_core.documents import Document
import logging

class WebSearchAgent:
    def __init__(self, max_retries: int = 3):
        self.max_retries = max_retries
        self.logger = logging.getLogger(__name__)
        
    def search(self, query: str, max_results: int = 3) -> List[Document]:
        try:
            search_results = ddg(query, max_results=max_results)
            
            # Convert to Document format
            documents = []
            for result in search_results:
                doc = Document(
                    page_content=result['body'],
                    metadata={
                        'source': result['link'],
                        'title': result['title'],
                        'is_web': True
                    }
                )
                documents.append(doc)
            
            self.logger.info(f"Found {len(documents)} web results")
            return documents
            
        except Exception as e:
            self.logger.error(f"DuckDuckGo search failed: {e}")
            return []