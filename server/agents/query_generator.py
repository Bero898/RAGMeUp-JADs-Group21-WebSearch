class SQLQueryGeneratorAgent:
    def __init__(self, llm_model):
        self.llm_model = llm_model  # The language model used for generating SQL queries

    def generate_sql_queries(self, questions):
        """
        Generate SQL queries for each of the subquestions.
        """
        prompt = f"Generate SQL queries for the following questions:\n\n" + "\n".join(questions)
        response = self.llm_model.generate(prompt)
        sql_queries = response.split('\n')  # Assuming each SQL query is on a new line
        return sql_queries