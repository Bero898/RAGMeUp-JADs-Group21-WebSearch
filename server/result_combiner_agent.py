# New file: result_combiner_agent.py
from typing import List, Dict
from langchain_core.documents import Document
from langchain.prompts import ChatPromptTemplate
from langchain.chains import LLMChain

class ResultCombinerAgent:
    def __init__(self, llm):
        self.llm = llm
        self.PromptTemplate(
            input_variables=["query", "db_info", "web_info"],
            template="""You are an assistant that combines information from database and web sources.

Question: {query}

Database Information:
{db_info}

Web Information:
{web_info}

Provide a comprehensive answer using the information from both sources. Prioritize the database information, but include relevant web details. Cite sources where appropriate.
"""
        )
        self.chain = LLMChain(llm=self.llm, prompt=self.combine_prompt)
        
    def combine_results(self, query: str, db_docs: List[Document], 
                       web_docs: List[Document]) -> Dict:
        db_text = "\n".join([d.page_content for d in db_docs])
        web_text = "\n".join([d.page_content for d in web_docs])
        
        self.llm.logger.info("Generating combined answer using LLM.")

        
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