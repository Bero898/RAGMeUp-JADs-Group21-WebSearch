class SubtopicIdentifierAgent:
    def __init__(self, llm_model):
        self.llm_model = llm_model  # The language model used for identifying subtopics

    def identify_subtopics(self, user_query):
        """
        Identify the main subtopics of the user's question.
        """
        prompt = f"Identify the main subtopics of the following question:\n\n{user_query}"
        response = self.llm_model.generate(prompt)
        subtopics = response.split('\n')  # Assuming each subtopic is on a new line
        return subtopics