class QuizCheckerAgent:
    def __init__(self, llm_model):
        self.llm_model = llm_model

    def check_answers(self, user_answers, generated_answers):
        feedback = []
        for user_answer, generated_answer in zip(user_answers, generated_answers):
            prompt = f"User's Answer: {user_answer}\nGenerated Answer: {generated_answer}\nProvide feedback on the user's answer."
            response = self.llm_model.generate(prompt)
            feedback.append(response)
        return feedback