class RelevanceCheckerAgent:
    def __init__(self, llm_model):
        self.llm_model = llm_model  # The language model used for checking relevance

    def check_relevance(self, questions, user_query):
        """
        Check whether the generated questions are relevant to the initial question.
        """
        prompt = f"User Query: {user_query}\nGenerated Questions:\n" + "\n".join(questions) + "\n\nCheck the relevance of each question."
        response = self.llm_model.generate(prompt)
        relevant_questions = response.split('\n')  # Assuming each relevant question is on a new line
        return relevant_questions