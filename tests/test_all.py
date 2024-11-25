import unittest
from server.agents.query_retriever import QueryRetrieverAgent

class MockRetriever:
    def retrieve(self, query):
        return ["Document 1 content", "Document 2 content"]

class TestQueryRetrieverAgent(unittest.TestCase):
    def setUp(self):
        self.agent = QueryRetrieverAgent(retriever=MockRetriever())

    def test_retrieve(self):
        query = "Test query"
        retrieved_docs = self.agent.retrieve(query)
        self.assertEqual(len(retrieved_docs), 2)
        self.assertIn("Document 1 content", retrieved_docs)

if __name__ == "__main__":
    unittest.main()
