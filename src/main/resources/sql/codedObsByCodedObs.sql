SELECT
  first_answers.answer_name,
  second_answers.answer_name,
  gender.gender,
  rag.name,
  rag.sort_order,
  sum(CASE WHEN first_concept.answer IS NOT NULL AND second_concept.answer IS NOT NULL AND p.gender IS NOT NULL THEN 1
      ELSE 0 END)
FROM
  (SELECT
     answer_concept AS answer,
     cn2.name       AS answer_name
   FROM concept_answer ca1
     INNER JOIN concept_name cn1 ON ca1.concept_id = cn1.concept_id AND cn1.name = 'Leprosy, Case Type' AND
                                    cn1.voided IS FALSE AND cn1.concept_name_type = 'FULLY_SPECIFIED'
     INNER JOIN concept_name cn2 ON ca1.answer_concept = cn2.concept_id AND
                                    cn2.voided IS FALSE AND cn2.concept_name_type = 'FULLY_SPECIFIED'
  ) first_answers
  INNER JOIN
  (SELECT
     answer_concept AS answer,
     cn2.name       AS answer_name
   FROM concept_answer ca1
     INNER JOIN concept_name cn1 ON ca1.concept_id = cn1.concept_id AND cn1.name = 'Leprosy, Leprosy Type' AND
                                    cn1.voided IS FALSE AND cn1.concept_name_type = 'FULLY_SPECIFIED'
     INNER JOIN concept_name cn2 ON ca1.answer_concept = cn2.concept_id AND
                                    cn2.voided IS FALSE AND cn2.concept_name_type = 'FULLY_SPECIFIED') second_answers
  INNER JOIN (SELECT DISTINCT gender
              FROM person
              WHERE gender IN ('M', 'F')) gender
  INNER JOIN reporting_age_group rag ON rag.report_group_name = 'Inpatient Discharge Reports'
  LEFT OUTER JOIN (
                    SELECT
                      o1.person_id,
                      cn2.concept_id AS    answer,
                      cn1.concept_id AS    question,
                      max(o1.obs_datetime) obsdatetime
                    FROM obs o1
                      INNER JOIN concept_name cn1
                        ON o1.concept_id = cn1.concept_id AND
                           cn1.concept_name_type = 'FULLY_SPECIFIED' AND cn1.name = 'Leprosy, Case Type'
                           AND o1.voided = 0 AND cn1.voided = 0
                      INNER JOIN concept_name cn2
                        ON o1.value_coded = cn2.concept_id
                           AND cn2.concept_name_type = 'FULLY_SPECIFIED'
                           AND cn2.voided = 0
                      WHERE o1.obs_datetime BETWEEN '2014-01-01' AND '2015-01-01'
                    GROUP BY o1.person_id, cn2.name
                  ) first_concept
    ON first_concept.answer = first_answers.answer
  LEFT OUTER JOIN (
                    SELECT
                      o1.person_id,
                      cn2.concept_id AS answer,
                      cn1.concept_id AS question
                    FROM obs o1
                      INNER JOIN concept_name cn1
                        ON o1.concept_id = cn1.concept_id AND
                           cn1.concept_name_type = 'FULLY_SPECIFIED'
                           AND cn1.name = 'Leprosy, Leprosy Type'
                           AND o1.voided = 0 AND cn1.voided = 0
                      INNER JOIN concept_name cn2
                        ON o1.value_coded = cn2.concept_id
                           AND cn2.concept_name_type = 'FULLY_SPECIFIED'
                           AND cn2.voided = 0
                    WHERE o1.obs_datetime BETWEEN '2014-01-01' AND '2015-03-01'
                  ) second_concept
    ON second_concept.answer = second_answers.answer
       AND first_concept.person_id = second_concept.person_id
  LEFT OUTER JOIN person p ON first_concept.person_id = p.person_id AND p.gender = gender.gender
                              AND cast(first_concept.obsdatetime AS DATE) BETWEEN (DATE_ADD(
    DATE_ADD(p.birthdate, INTERVAL rag.min_years YEAR), INTERVAL rag.min_days
    DAY)) AND (DATE_ADD(DATE_ADD(p.birthdate, INTERVAL rag.max_years YEAR), INTERVAL
                        rag.max_days DAY))
GROUP BY first_answers.answer_name, second_answers.answer_name, gender.gender, rag.name, rag.sort_order;