package controllers;

import models.Computer;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

public class JApplication extends Controller {

	@Transactional(readOnly = true)
	public static Result find(long id) {
//		return ok(JPA.em().find(Computer.class, id).toString());
		return ok("truc");
	}

	@Transactional
	public static Result create() {
		Computer computer = new Computer();
		computer.name = "new name " + System.currentTimeMillis();

		computer.save();

		return ok(computer.toString());
	}

	// /**
	// * Display the paginated list of computers.
	// *
	// * @param page
	// * Current page number (starts from 0)
	// * @param sortBy
	// * Column to be sorted
	// * @param order
	// * Sort order (either asc or desc)
	// * @param filter
	// * Filter applied on computer names
	// */
	// @Transactional(readOnly = true)
	// public static Result list(int page, String sortBy, String order,
	// String filter) {
	// return ok(Computer.page(page, 10, sortBy, order, filter).toString());
	// }

	// /**
	// * Display the 'edit form' of a existing Computer.
	// *
	// * @param id
	// * Id of the computer to edit
	// */
	// @Transactional(readOnly = true)
	// public static Result edit(Long id) {
	// Form<Computer> computerForm = form(Computer.class).fill(
	// Computer.findById(id));
	// return ok(editForm.render(id, computerForm));
	// }
	//
	// /**
	// * Handle the 'edit form' submission
	// *
	// * @param id
	// * Id of the computer to edit
	// */
	// @Transactional
	// public static Result update(Long id) {
	// Form<Computer> computerForm = form(Computer.class).bindFromRequest();
	// if (computerForm.hasErrors()) {
	// return badRequest(editForm.render(id, computerForm));
	// }
	// computerForm.get().update(id);
	// flash("success", "Computer " + computerForm.get().name
	// + " has been updated");
	// return GO_HOME;
	// }
	//
	// /**
	// * Display the 'new computer form'.
	// */
	// @Transactional(readOnly = true)
	// public static Result create() {
	// Form<Computer> computerForm = form(Computer.class);
	// return ok(createForm.render(computerForm));
	// }
	//
	// /**
	// * Handle the 'new computer form' submission
	// */
	// @Transactional
	// public static Result save() {
	// Form<Computer> computerForm = form(Computer.class).bindFromRequest();
	// if (computerForm.hasErrors()) {
	// return badRequest(createForm.render(computerForm));
	// }
	// computerForm.get().save();
	// flash("success", "Computer " + computerForm.get().name
	// + " has been created");
	// return GO_HOME;
	// }
	//
	// /**
	// * Handle computer deletion
	// */
	// @Transactional
	// public static Result delete(Long id) {
	// Computer.findById(id).delete();
	// flash("success", "Computer has been deleted");
	// return GO_HOME;
	// }

}