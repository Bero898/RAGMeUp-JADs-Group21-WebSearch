# web_search_agent.py

from typing import List
from duckduckgo_search import DDGS
from langchain.schema import Document
import logging

class WebSearchAgent:
    def __init__(self, max_retries: int = 3):
        self.max_retries = max_retries
        self.logger = logging.getLogger(__name__)
            
    def search(self, query: str, max_results: int = 5) -> List[Document]:
        try:
            self.logger.info(f"Performing web search for query: {query}")
            ddgs = DDGS()
            search_results = ddgs.text(query, max_results=max_results)
            if not search_results:
                self.logger.warning("No results found from web search.")
                return []
            documents = []
            for result in search_results:
                doc = Document(
                    page_content=result.get('body', ''),
                    metadata={
                        'source': result.get('href', ''),
                        'title': result.get('title', ''),
                        'is_web': True
                    }
                )
                documents.append(doc)
            self.logger.info(f"Found {len(documents)} web results")
            return documents
        except Exception as e:
            self.logger.error(f"DuckDuckGo search failed: {e}")
            return []