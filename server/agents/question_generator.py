class QuestionGeneratorAgent:
    def __init__(self, llm_model):
        self.llm_model = llm_model  # The language model used for generating questions

    def generate_questions(self, subtopics):
        """
        Generate questions regarding the subtopics.
        """
        prompt = f"Generate questions regarding the following subtopics:\n\n" + "\n".join(subtopics)
        response = self.llm_model.generate(prompt)
        questions = response.split('\n')  # Assuming each question is on a new line
        return questions