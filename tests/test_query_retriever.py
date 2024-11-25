# Importing the module directly since the tests folder is now at the project root level
from server.agents.query_retriever import QueryRetrieverAgent

class MockRetriever:
    def retrieve(self, query):
        # Mocked retrieval logic
        return [f"Document content for '{query}' - 1", f"Document content for '{query}' - 2"]

if __name__ == "__main__":
    mock_retriever = MockRetriever()
    retriever_agent = QueryRetrieverAgent(retriever=mock_retriever)
    query = "Explain natural language processing"
    retrieved_docs = retriever_agent.retrieve(query)
    print("Retrieved Documents:", retrieved_docs)


