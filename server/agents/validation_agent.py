class ValidationAgent:
    def __init__(self, validation_model=None):
        self.validation_model = validation_model

    def validate(self, documents):
        """
        Validate the retrieved documents.
        This could be as simple as applying a filter or a model to score relevance.
        """
        valid_docs = []
        for doc in documents:
            # Example: if validation_model is defined, use it to check relevance
            if self.validation_model is None or self.validation_model.is_valid(doc):
                valid_docs.append(doc)
        return valid_docs

