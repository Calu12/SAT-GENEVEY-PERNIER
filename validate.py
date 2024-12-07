import os
import subprocess

# Chemins vers les dossiers et fichiers
BASE_DIR = r""
VAL_EXECUTABLE = r"bin\\validate.exe"  # Remplacez par le chemin vers validate.exe

# Dictionnaire des sous-dossiers de domaine
DOMAINS = {
    "blocks": "blocks",
    "depots": "depots",
    "gripper": "gripper",
    "logistics": "logistics"
}

# Dictionnaire des fichiers de sortie par domaine
OUTPUT_FILES = {
    "blocks": os.path.join(BASE_DIR, "validationBlocks.txt"),
    "depots": os.path.join(BASE_DIR, "validationDepots.txt"),
    "gripper": os.path.join(BASE_DIR, "validationGripper.txt"),
    "logistics": os.path.join(BASE_DIR, "validationLogistics.txt")
}

def validate_plan(domain_file, problem_file, plan_file):
    """
    Valide un plan donné en utilisant l'outil VAL.
    """
    try:
        result = subprocess.run(
            [VAL_EXECUTABLE, domain_file, problem_file, plan_file],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            shell=True
        )
        if "Plan valid" in result.stdout:
            return 1, result.stdout  # Retourne 1 pour un plan valide
        else:
            return 0, result.stdout  # Retourne 0 pour un plan invalide
    except FileNotFoundError:
        return 0, "Erreur : L'exécutable VAL est introuvable."

def process_domain(domain_name, domain_folder, output_file):
    """
    Traite tous les problèmes et plans pour un domaine donné.
    """
    domain_file = os.path.join(BASE_DIR, domain_folder, "domain.pddl")
    problem_files = [f for f in os.listdir(os.path.join(BASE_DIR, domain_folder)) if f.startswith("problem") and f.endswith(".pddl")]
    plan_files = [f for f in os.listdir(os.path.join(BASE_DIR, "plan" + domain_name.capitalize())) if f.startswith("resproblem") and f.endswith(".pddl")]

    if not problem_files or not plan_files:
        print(f"Aucun fichier problème ou plan trouvé pour {domain_name}.")
        return []

    # Associer les fichiers problème aux plans
    problem_plan_pairs = []
    for problem_file in problem_files:
        problem_number = problem_file.replace("problem", "").replace(".pddl", "")
        corresponding_plan = f"resproblem{problem_number}.pddl"
        if corresponding_plan in plan_files:
            problem_plan_pairs.append((problem_file, corresponding_plan))

    if not problem_plan_pairs:
        print(f"Aucune correspondance entre problèmes et plans pour {domain_name}.")
        return []

    # Validation des plans et écriture dans le fichier de sortie
    domain_results = []
    for problem_file, plan_file in problem_plan_pairs:
        problem_path = os.path.join(BASE_DIR, domain_folder, problem_file)
        plan_path = os.path.join(BASE_DIR, "plan" + domain_name.capitalize(), plan_file)
        is_valid, _ = validate_plan(domain_file, problem_path, plan_path)
        domain_results.append((problem_file, is_valid))
    
    # Ecrire les résultats dans le fichier de sortie
    with open(output_file, "a") as report:
        for problem_file, is_valid in domain_results:
            # Ajouter le nom du fichier problème et l'entier (1 ou 0) au fichier
            report.write(f"{problem_file}: {is_valid}\n")
            print(f"{problem_file}: {'1 (VALIDE)' if is_valid == 1 else '0 (INVALIDE)'}")
    
    return domain_results

def main():
    # Traite chaque domaine et écrit dans les fichiers de sortie correspondants
    for domain_name, domain_folder in DOMAINS.items():
        output_file = OUTPUT_FILES[domain_name]
        print(f"Validation des plans pour le domaine {domain_name}...")
        process_domain(domain_name, domain_folder, output_file)

if __name__ == "__main__":
    main()
