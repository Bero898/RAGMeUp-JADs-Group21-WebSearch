class AnswerGeneratorAgent:
    def __init__(self, llm_model):
        self.llm_model = llm_model  # The language model used for generating answers

    def generate(self, prompt, validated_docs, history):
        """
        Generate a response based on the prompt, validated documents, and conversation history.
        """
        # Use the llm_model to generate a response based on the provided inputs
        response = self.llm_model.generate(prompt, context=validated_docs + history)
        return {
            "answer": response,
            "history": history + [{"role": "user", "content": prompt}, {"role": "teacher", "content": response}]
        }


