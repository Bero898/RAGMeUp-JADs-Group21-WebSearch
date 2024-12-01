# New file: web_search_agent.py
from typing import List, Dict
from tavily import TavilyClient
from langchain_core.documents import Document

class WebSearchAgent:
    def __init__(self, api_key: str):
        print("Initializing WebSearchAgent with API key:", api_key)
        self.client = TavilyClient(api_key=api_key)
        if self.client.api_key == api_key:
            print("API key is being used correctly.")
        else:
            print("API key is not being used correctly.")
        
    def search(self, query: str, max_results: int = 3) -> List[Document]:
        search_results = self.client.search(
            query=query,
            search_depth="advanced",
            max_results=max_results
        )
        
        # Convert to Document format
        documents = []
        for result in search_results:
            doc = Document(
                page_content=result['content'],
                metadata={
                    'source': result['url'],
                    'title': result['title'],
                    'score': result['score'],
                    'is_web': True
                }
            )
            documents.append(doc)
        
        return documents