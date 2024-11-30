from server.agents.answer_generator import AnswerGeneratorAgent

class MockLLMModel:
    def generate(self, prompt, context):
        # Generate a simple answer combining the prompt and context
        return f"Answer based on prompt: '{prompt}' and context of {len(context)} documents."

if __name__ == "__main__":
    mock_llm = MockLLMModel()
    answer_generator_agent = AnswerGeneratorAgent(llm_model=mock_llm)
    prompt = "What is RAG?"
    validated_docs = ["Document 1 content", "Document 2 content"]
    history = [{"role": "user", "content": "Hello"}, {"role": "assistant", "content": "Hi, how can I assist you?"}]
    
    response_data = answer_generator_agent.generate(prompt, validated_docs, history)
    print("Generated Response:", response_data['answer'])
