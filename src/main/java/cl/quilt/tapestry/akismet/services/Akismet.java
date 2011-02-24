/**
 * $Id$
 * 
 * /**
 * Created by Raúl Montes, Quilt Technologies Ltda.
 * and released under The BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * Copyright (c) 2011, Raúl Montes
 * All rights reserved.
 *
 * Redistribution  and  use  in  source   and  binary  forms,  with  or   without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain   the above copyright   notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary  form must reproduce  the above copyright  notice,
 *   this list of conditions  and the following  disclaimer in the  documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name  of  Quilt Technologies Ltda. nor the names  of its contributors
 *   may be used  to endorse   or promote  products derived  from  this  software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS  PROVIDED BY THE  COPYRIGHT HOLDERS AND  CONTRIBUTORS "AS IS"
 * AND ANY  EXPRESS OR  IMPLIED WARRANTIES,  INCLUDING, BUT  NOT LIMITED  TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL  THE COPYRIGHT HOLDER OR CONTRIBUTORS  BE LIABLE
 * FOR ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL,  EXEMPLARY, OR  CONSEQUENTIAL
 * DAMAGES (INCLUDING,  BUT NOT  LIMITED TO,  PROCUREMENT OF  SUBSTITUTE GOODS OR
 * SERVICES; LOSS  OF USE,  DATA, OR  PROFITS; OR  BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT  LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE  USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package cl.quilt.tapestry.akismet.services;

import ac.simons.akismet.AkismetException;

/**
 * @author raul
 * 
 */
public interface Akismet<T> {

	/**
	 * Tells if the service will verify the comments
	 * 
	 * @return true if the service is active, false otherwise
	 */
	public boolean isEnabled();

	/**
	 * Enable or disable the service
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled);

	/**
	 * This is basically the core of everything. This call takes a number of
	 * arguments and characteristics about the submitted content and then
	 * returns a thumbs up or thumbs down.<br>
	 * Almost everything is optional, but performance can drop dramatically if
	 * you exclude certain elements.<br>
	 * I would recommend erring on the side of too much data, as everything is
	 * used as part of the Akismet signature.
	 * 
	 * @return True, if the comment is spam, false otherwise
	 * @throws AkismetException
	 */
	public boolean commentCheck(final T comment) throws AkismetException;
	
	//TODO: hacer una typecoercion de un X tipo de comentario a un AkismetComment.
	// quizá es mejor tratar que, de alguna forma, se pueda contribuir una transformación y que la interfaz mágicamente (quizá con generics)
	// pueda funcionar con esos tipo de comentarios, transformándolos.

	/**
	 * This call is for submitting comments that weren't marked as spam but
	 * should have been.
	 * 
	 * @param comment
	 * @return True if the spam was successfully submitted.
	 * @throws AkismetException
	 */
	public boolean submitSpam(final T comment) throws AkismetException;

	/**
	 * This call is intended for the marking of false positives, things that
	 * were incorrectly marked as spam.
	 * 
	 * @param comment
	 * @return True if the ham was successfully submitted.
	 * @throws AkismetException
	 */
	public boolean submitHam(final T comment) throws AkismetException;

}