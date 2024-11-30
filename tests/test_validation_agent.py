from server.agents.validation_agent import ValidationAgent

class MockValidationModel:
    def is_valid(self, doc):
        # Example simple rule: only documents containing 'valid' are considered relevant
        return "valid" in doc.lower()

if __name__ == "__main__":
    mock_validation_model = MockValidationModel()
    validation_agent = ValidationAgent(validation_model=mock_validation_model)
    documents = ["This is a valid document.", "This document is not relevant."]
    valid_docs = validation_agent.validate(documents)
    print("Validated Documents:", valid_docs)
