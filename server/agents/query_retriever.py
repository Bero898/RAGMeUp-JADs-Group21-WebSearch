class QueryRetrieverAgent:
    def __init__(self, retriever):
        self.retriever = retriever  # This could be BM25, a vector store, etc.

    def retrieve(self, query):
        """
        Retrieve relevant documents based on the query.
        """
        results = self.retriever.retrieve(query)
        return results
