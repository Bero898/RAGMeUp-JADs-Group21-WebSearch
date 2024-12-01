# New file: result_combiner_agent.py
from typing import List, Dict
from langchain_core.documents import Document
from langchain.prompts import ChatPromptTemplate
from langchain.chains import LLMChain

class ResultCombinerAgent:
    def __init__(self, llm):
        self.llm = llm
        self.combine_prompt = ChatPromptTemplate.from_messages([
            ("system", """Combine information from database and web sources. 
             Prioritize database information but include relevant web details.
             Format output with clear source attribution."""),
            ("human", "{query}\n\nDatabase info: {db_info}\n\nWeb info: {web_info}")
        ])
        self.chain = LLMChain(llm=self.llm, prompt=self.combine_prompt)
        
    def combine_results(self, query: str, db_docs: List[Document], 
                       web_docs: List[Document]) -> Dict:
        db_text = "\n".join([d.page_content for d in db_docs])
        web_text = "\n".join([d.page_content for d in web_docs])
        
        result = self.chain.run(
            query=query,
            db_info=db_text,
            web_info=web_text
        )
        
        # Combine documents for frontend display
        all_docs = []
        for doc in db_docs:
            doc.metadata['is_web'] = False
            all_docs.append(doc)
        for doc in web_docs:
            doc.metadata['is_web'] = True
            all_docs.append(doc)
            
        return {
            "answer": result,
            "documents": all_docs,
            "query": query
        }